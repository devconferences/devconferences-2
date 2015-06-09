const React = require('react');
const _ = require('underscore');

const alwaysTrue = (val) => true;
const validators = {
    text: function(val) { return true; },
    password: function(val) { return true; },
    datetime: alwaysTrue,
    date: alwaysTrue,
    month: alwaysTrue,
    time: alwaysTrue,
    week: alwaysTrue,
    number: function(val) { return /[0-9]+/.test(val); },
    email: alwaysTrue,
    url: alwaysTrue,
    search: alwaysTrue,
    tel: alwaysTrue,
    color: alwaysTrue
};

const FormGroup = React.createClass({
    componentWillMount() {
        this.validator = validators[this.props.type] || function() { return true; };
    },
    change: function(e) {
        e.preventDefault();
        if (this.validator(e.target.value)) {
            this.props.modelMutator(this.props.name, e.target.value);
        } else {
            // mark as error
        }
    },
    render() {
        return (
            <div className="form-group">
                <label htmlFor={this.props.key} className="col-sm-2 control-label">{this.props.type.label || this.props.name}</label>
                <div className="col-sm-10">
                    <input
                        type={this.props.type.type || ''}
                        className="form-control"
                        id={this.props.key}
                        placeholder={this.props.type.placeholder || ''}
                        value={this.props.modelMutator(this.props.name)}
                        onChange={this.change} />
                </div>
            </div>
        );
    }
});

export default React.createClass({
    getInitialState() {
        return {
            model: {}
        }
    },
    componentWillMount() {
        this.contract = _.clone(this.props.contract || {});
        if (_.isArray(this.contract)) {
            _.each(this.contract, c => {
                c.key = _.uniqueId('FormGroup_');
            });
        } else {
            this.contract = _.map(_.keys(this.contract), key => {
                this.contract[key].key = _.uniqueId('FormGroup_');
                this.contract[key].name = key;
                return this.contract[key];
            });
        }
        this.setState({
            model: this.props.model || {}
        });
    },
    validator(name, value) {

    },
    modelMutator(name, value) {
        let model = this.state.model;
        if (!value) {
            return model[name];
        }
        model[name] = value;
        this.setState({
            model: model
        });
    },
    groups() {
        return _.map(this.contract, g => <FormGroup key={g.key} name={g.name}  type={g} validator={validator} modelMutator={this.modelMutator}/>);
    },
    cancel(e) {
        e.preventDefault();
        (this.props.cancelCallback || function() {})(this.state.model);
    },
    perform(e) {
        e.preventDefault();
        (this.props.performCallback || function() {})(this.state.model);
    },
    render() {
        return (
            <form className="form-horizontal">
                {this.groups()}
                <div className="form-group">
                    <div className="col-sm-offset-2 col-sm-10">
                        <div className="btn-group" role="group">
                            <button type="button" className="btn btn-default" onClick={this.cancel}>{this.props.cancelTitle || 'Cancel'}</button>
                            <button type="button" className="btn btn-primary" onClick={this.perform}>{this.props.performTitle || 'OK'}</button>
                        </div>
                    </div>
                </div>
            </form>
        );
    }
});


/**

 var ModelForm = require('./components/form');

 var model = {
    name: 'John',
    surname: 'Doe'
};

 var contract = [
 {
     name: 'name',
     label: 'Nom',
     type: 'text'
 },
 {
     label: 'Surname',
     name: 'surname',
     type: 'text'
 },
 {
     name: 'email',
     label: 'Email address',
     type: 'email',
     placeholder: "john.doe@gmail.com"
 },
 {
     name: 'age',
     type: 'number'
 }
 ];

 function performCallback(m) {
    console.log(m);
}

 React.render(<ModelForm model={model} contract={contract} performCallback={performCallback} />, document.body);

 **/
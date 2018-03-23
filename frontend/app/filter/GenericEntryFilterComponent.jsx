import React from 'react';
import PropTypes from 'prop-types';
import Omit from 'lodash.omit';
import {validateVsid} from "../validators/VsidValidator.jsx";
import FilterTypeSelection from './FilterTypeSelection.jsx';

class GenericEntryFilterComponent extends React.Component {
    static propTypes = {
        filterDidUpdate: PropTypes.func.isRequired, //this is called when the filter state should be updated. Passed a
                                                    //key-value object of the terms.
        isAdmin: PropTypes.bool,
        filterTerms: PropTypes.object.isRequired
    };

    constructor(props){
        super(props);

        this.filterSpec = [
            {
                key: "title",
                label: "Title",
                //this is a called for every update. if it returns anything other than NULL it's considered an
                //error and displayed alongside the control
                validator: (input)=>null
            },
            {
                key: "vidispineId",
                label: "PLUTO project id",
                //validator: (input)=>vsidValidator.test(input) ? null : "This must be in the form of XX-nnnnn"
                validator: validateVsid
            },
            {
                key: "filename",
                label: "Project file name",
                validator: (input)=>null
            }
        ];

        this.state = {
            fieldErrors: {},
            showFilters: false,
            matchType: "W_CONTAINS"
        };

        this.switchHidden = this.switchHidden.bind(this);
        this.doClear = this.doClear.bind(this);
    }

    updateFilters(filterKey, value,cb){
        let newFilters = {};
        newFilters[filterKey] = value;

        this.props.filterDidUpdate(Object.assign(this.props.filterTerms,{match: this.state.matchType}, newFilters));
    }

    addFieldError(filterKey, errorDesc, cb){
        let newFilters = {};
        newFilters[filterKey] = errorDesc;
        this.setState({fieldErrors: newFilters}, cb);
    }

    removeFieldError(filterKey, cb){
        this.setState({fieldErrors: Omit(this.state.fieldErrors, filterKey)}, cb);
    }

    entryUpdated(event, filterKey){
        const spec = this.filterSpec.filter(entry=>entry.key===filterKey);
        const newValue = event.target.value.trim();

        if(spec[0].validator){
            const wasError = spec[0].validator(newValue);
            if(wasError){
                this.addFieldError(filterKey, wasError);
            } else {
                this.removeFieldError(filterKey);
                this.updateFilters(filterKey, spec[0].converter ? spec[0].converter(newValue) : newValue);
            }
        } else {
            this.updateFilters(filterKey,newValue);
        }

    }

    switchHidden(){
        this.setState({showFilters: !this.state.showFilters});
    }

    doClear(){
        this.props.filterDidUpdate(Object.assign({},{match: this.state.matchType}));
    }

    showFilterError(fieldName){
        return <span>{this.state.fieldErrors[fieldName] ? this.state.fieldErrors[fieldName] : ""}</span>
    }

    controlFor(filterEntry){
        const disabled = (!this.props.isAdmin) && filterEntry.disabledIfNotAdmin;

        if(filterEntry.valuesStateKey && this.state.hasOwnProperty(filterEntry.valuesStateKey)){
            return <select disabled={disabled} id={filterEntry.key}
                           onChange={event=>this.entryUpdated(event, filterEntry.key)}
                           value={this.props.filterTerms[filterEntry.key]}>
                {
                    this.state[filterEntry.valuesStateKey].map(value=><option key={value} name={value}>{value}</option>)
                }
            </select>
        } else {
            return <input disabled={disabled} id={filterEntry.key}
                          onChange={(event) => this.entryUpdated(event, filterEntry.key)}
                          value={this.props.filterTerms[filterEntry.key]}/>
        }
    }

    render(){
        return <div className="filter-list-block">
            <span className="filter-list-title">
                <i className="fa fa-search-plus" style={{marginRight: "0.5em"}}/>
                Filters
                <a className="filter-link-control" onClick={this.switchHidden}>{this.state.showFilters ? "hide" : "show"}</a>
            </span>

            {this.filterSpec.map(filterEntry=>
                <span className="filter-list-entry" style={{ display: this.state.showFilters ? "inline-block" : "none" }}  key={filterEntry.key}>
                   <label className="filter-entry-label" htmlFor={filterEntry.key}>{filterEntry.label}</label>
                    <div className="filter-entry-input">
                        {this.controlFor(filterEntry)}
                        {this.state.fieldErrors[filterEntry.key] ? <i className="fa fa-exclamation" style={{color: "red", marginLeft: "0.5em"}}/> : <i className="fa fa-check" style={{color: "green", marginLeft: "0.5em"}}/>}
                        <br style={{marginTop: "5px" }}/>
                        {this.state.fieldErrors[filterEntry.key] ? <span className="filter-entry-error">{this.state.fieldErrors[filterEntry.key]}</span> : <span/>}
                    </div>
                </span>)}

            <FilterTypeSelection showFilters={this.state.showFilters} type={this.state.matchType} selectionChanged={newValue=>this.setState({matchType: newValue})}/>
            <span className="filter-list-entry" style={{ display: this.state.showFilters ? "inline-block": "none", float: "right"}}>
                <button onClick={this.doClear}>Clear</button>
            </span>
        </div>
    }
}

export default GenericEntryFilterComponent;

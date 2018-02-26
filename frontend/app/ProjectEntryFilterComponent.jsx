import React from 'react';
import PropTypes from 'prop-types';
import Omit from 'lodash.omit';

class ProjectEntryFilterComponent extends React.Component {
    static propTypes = {
        filterDidUpdate: PropTypes.func.isRequired //this is called when the filter state should be updated. Passed a
                                                    //key-value object of the terms.
    };

    constructor(props){
        super(props);

        const vsidValidator = new RegExp('\\w{2}-\\d+');

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
                validator: (input)=>{
                    const result=vsidValidator.test(input);
                    console.log("validation result for '"+ input, "': ", result);
                    return result ? null : "This must be in the form of XX-nnnnn"
                }
            },
            {
                key: "filename",
                label: "Project file name",
                validator: (input)=>null
            }
        ];

        this.state = {
            filters: {},
            fieldErrors: {},
            showFilters: false
        };

        this.switchHidden = this.switchHidden.bind(this);
        this.doUpdate = this.doUpdate.bind(this);
        this.doClear = this.doClear.bind(this);
    }

    updateFilters(filterKey, value,cb){
        let newFilters = {match: "W_ENDSWITH"};
        newFilters[filterKey] = value;
        this.setState({filters: Object.assign(this.state.filters, newFilters)}, cb);
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

        if(spec[0].validator){
            const wasError = spec[0].validator(event.target.value);
            if(wasError){
                this.addFieldError(filterKey, wasError);
            } else {
                this.removeFieldError(filterKey);
                this.updateFilters(filterKey, event.target.value);
            }
        } else {
            this.updateFilters(filterKey, event.target.value);
        }

    }

    switchHidden(){
        this.setState({showFilters: !this.state.showFilters});
    }

    doUpdate(){
        this.props.filterDidUpdate(this.state.filters);
    }

    doClear(){
        this.setState({
            filters: {match: "W_ENDSWITH"},
        }, ()=>this.doUpdate());
    }

    showFilterError(fieldName){
        return <span>{this.state.fieldErrors[fieldName] ? this.state.fieldErrors[fieldName] : ""}</span>
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
                        <input id={filterEntry.key} onChange={(event)=>this.entryUpdated(event, filterEntry.key)} value={this.state.filters[filterEntry.key]}/>
                        {this.state.fieldErrors[filterEntry.key] ? <i className="fa fa-exclamation" style={{color: "red", marginLeft: "0.5em"}}/> : <i className="fa fa-check" style={{color: "green", marginLeft: "0.5em"}}/>}
                        <br style={{marginTop: "5px" }}/>
                        {this.state.fieldErrors[filterEntry.key] ? <span className="filter-entry-error">{this.state.fieldErrors[filterEntry.key]}</span> : <span/>}
                    </div>
                </span>)}
            <span className="filter-list-entry" style={{ display: this.state.showFilters ? "inline-block": "none", float: "right"}}>
                <button onClick={this.doClear}>Clear</button>
                <button onClick={this.doUpdate}>Refilter</button>
            </span>
        </div>
    }
}

export default ProjectEntryFilterComponent;

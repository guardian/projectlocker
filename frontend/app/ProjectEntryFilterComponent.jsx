import React from 'react';
import PropTypes from 'prop-types';

class ProjectEntryFilterComponent extends React.Component {
    static propTypes = {
        filterDidUpdate: PropTypes.func.isRequired //this is called when the filter state should be updated. Passed a
                                                    //key-value object of the terms.
    };

    constructor(props){
        super(props);

        const vsidValidator = new RegExp('^\w{2}-\d+$|^$');

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
                validator: (input)=>vsidValidator.test(input) ? null : "This must be in the form of XX-nnnnn"
            },
            {
                key: "filename",
                label: "Project file name",
                validator: (input)=>null
            }
        ];

        this.state = {
            filters: {},
            showFilters: true
        };

        this.switchHidden = this.switchHidden.bind(this);
        this.doUpdate = this.doUpdate.bind(this);
        this.doClear = this.doClear.bind(this);
    }

    entryUpdated(event, filterKey){
        let newFilters = {};
        newFilters[filterKey] = event.target.value;
        this.setState({filters: Object.assign(this.state.filters, newFilters)}, ()=>{

        });
    }

    switchHidden(){
        this.setState({showFilters: !this.state.showFilters});
    }

    doUpdate(){
        this.props.filterDidUpdate(this.state.filters);
    }

    doClear(){
        this.setState({
            filters: {},
        }, ()=>this.doUpdate());
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
                   <input className="filter-entry-input" id={filterEntry.key} onChange={(event)=>this.entryUpdated(event, filterEntry.key)} value={this.state.filters[filterEntry.key]}/>
                </span>)}
            <span className="filter-list-entry" style={{ display: this.state.showFilters ? "inline-block": "none", float: "right"}}>
                <button onClick={this.doClear}>Clear</button>
                <button onClick={this.doUpdate}>Refilter</button>
            </span>
        </div>
    }
}

export default ProjectEntryFilterComponent;
